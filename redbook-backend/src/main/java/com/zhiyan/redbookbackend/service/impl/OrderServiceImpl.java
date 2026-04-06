package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.OrderCreateDirectDTO;
import com.zhiyan.redbookbackend.dto.req.OrderCreateFromCartDTO;
import com.zhiyan.redbookbackend.dto.resp.DirectBuyPreviewVO;
import com.zhiyan.redbookbackend.dto.resp.OrderDetailVO;
import com.zhiyan.redbookbackend.dto.resp.OrderLineVO;
import com.zhiyan.redbookbackend.dto.resp.OrderListProductPreviewVO;
import com.zhiyan.redbookbackend.dto.resp.OrderListRowVO;
import com.zhiyan.redbookbackend.entity.Cart;
import com.zhiyan.redbookbackend.entity.OrderItem;
import com.zhiyan.redbookbackend.entity.Product;
import com.zhiyan.redbookbackend.entity.ShopOrder;
import com.zhiyan.redbookbackend.entity.UserCoupon;
import com.zhiyan.redbookbackend.entity.UserFollow;
import com.zhiyan.redbookbackend.exception.BizException;
import com.zhiyan.redbookbackend.mapper.CartMapper;
import com.zhiyan.redbookbackend.mapper.OrderItemMapper;
import com.zhiyan.redbookbackend.mapper.ShopOrderMapper;
import com.zhiyan.redbookbackend.mapper.UserCouponMapper;
import com.zhiyan.redbookbackend.mapper.UserFollowMapper;
import com.zhiyan.redbookbackend.service.IOrderService;
import com.zhiyan.redbookbackend.service.IProductService;
import com.zhiyan.redbookbackend.service.IUserWalletService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements IOrderService {

    private static final int ORDER_CREATED = 0;
    private static final int ORDER_PAID = 1;
    private static final int ORDER_CANCELLED = 2;
    private static final int ORDER_REFUNDED = 3;
    private static final long PAY_TIMEOUT_MINUTES = 15;
    private static final long REFUND_WINDOW_HOURS = 1;
    private static final int COUPON_UNUSED = 0;
    private static final int COUPON_USED = 1;
    private static final int PRODUCT_ONLINE = 1;
    /** 模拟支付密码（前端默认填入，后端校验） */
    private static final String MOCK_PAY_PASSWORD = "123456";

    private record LineAgg(long productId, int qty, long unitPriceCent) {
        long subtotal() {
            return unitPriceCent * qty;
        }
    }

    @Resource
    private CartMapper cartMapper;
    @Resource
    private ShopOrderMapper shopOrderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private IProductService productService;
    @Resource
    private IUserWalletService userWalletService;
    @Resource
    private UserCouponMapper userCouponMapper;
    @Resource
    private UserFollowMapper userFollowMapper;
    @Resource
    private PlatformTransactionManager transactionManager;

    private long computeCouponDiscount(UserCoupon c, long totalCent, List<LineAgg> aggs) {
        if (!Objects.equals(c.getStatus(), COUPON_UNUSED)) {
            throw new BizException("优惠券不可用");
        }
        if (c.getExpireTime() != null && !c.getExpireTime().isAfter(LocalDateTime.now())) {
            throw new BizException("优惠券已过期");
        }
        long minOrder = c.getMinOrderCent() != null ? c.getMinOrderCent() : 0L;
        long unitDisc = c.getDiscountCent() != null ? c.getDiscountCent() : 0L;
        Long bindPid = c.getProductId();
        if (bindPid == null) {
            if (totalCent < minOrder) {
                throw new BizException("未达到优惠券使用门槛");
            }
            return Math.min(unitDisc, totalCent);
        }
        long matchSub = 0L;
        int matchQty = 0;
        for (LineAgg a : aggs) {
            if (Objects.equals(a.productId, bindPid)) {
                matchSub += a.subtotal();
                matchQty += a.qty;
            }
        }
        if (matchSub == 0L) {
            throw new BizException("订单不含该优惠券适用商品");
        }
        if (matchSub < minOrder) {
            throw new BizException("未达到优惠券使用门槛");
        }
        return Math.min(unitDisc * matchQty, matchSub);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createFromCart(OrderCreateFromCartDTO dto) {
        if (dto == null) {
            dto = new OrderCreateFromCartDTO();
        }
        Long uid = UserHolder.getUser().getId();
        List<Cart> lines = cartMapper.selectList(Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, uid));
        if (lines.isEmpty()) {
            return Result.fail("购物车为空");
        }
        List<LineAgg> aggs = new ArrayList<>();
        for (Cart line : lines) {
            Product p = productService.getById(line.getProductId());
            if (p == null) {
                return Result.fail("商品不存在: " + line.getProductId());
            }
            int q = line.getQuantity() != null ? line.getQuantity() : 0;
            if (p.getStock() == null || p.getStock() < q) {
                return Result.fail("库存不足: " + p.getTitle());
            }
            long pc = p.getPriceCent() != null ? p.getPriceCent() : 0L;
            aggs.add(new LineAgg(p.getId(), q, pc));
        }
        long totalCent = 0L;
        for (LineAgg a : aggs) {
            totalCent += a.subtotal();
        }

        long discountCent = 0L;
        Long userCouponId = null;
        if (dto.getUserCouponId() != null) {
            UserCoupon c = userCouponMapper.selectById(dto.getUserCouponId());
            if (c == null || !Objects.equals(c.getUserId(), uid)) {
                throw new BizException("优惠券不存在");
            }
            discountCent = computeCouponDiscount(c, totalCent, aggs);
            userCouponId = c.getId();
        }

        long payCent = totalCent - discountCent;

        ShopOrder order = new ShopOrder();
        order.setUserId(uid);
        order.setTotalCent(totalCent);
        order.setPayCent(payCent);
        order.setDiscountCent(discountCent);
        order.setUserCouponId(userCouponId);
        order.setStatus(ORDER_CREATED);
        order.setFromCart(1);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.insert(order);

        for (Cart line : lines) {
            Product p = productService.getById(line.getProductId());
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setQuantity(line.getQuantity());
            oi.setPriceCent(p.getPriceCent());
            orderItemMapper.insert(oi);
        }
        return Result.ok(order.getId());
    }

    @Override
    public Result directBuyPreview(long productId, int quantity, Long fallbackSellerUserId) {
        if (quantity < 1) {
            return Result.fail("数量无效");
        }
        Long uid = UserHolder.getUser().getId();
        Product p = productService.getById(productId);
        if (p == null || !Objects.equals(p.getStatus(), PRODUCT_ONLINE)) {
            return Result.fail("商品不可用");
        }
        long unit = p.getPriceCent() != null ? p.getPriceCent() : 0L;
        long totalCent = unit * quantity;
        Long sellerId = p.getSellerId();
        if (sellerId == null && fallbackSellerUserId != null) {
            sellerId = fallbackSellerUserId;
        }
        boolean following = false;
        if (sellerId != null) {
            following = userFollowMapper.selectCount(Wrappers.<UserFollow>lambdaQuery()
                    .eq(UserFollow::getFollowerId, uid)
                    .eq(UserFollow::getFolloweeId, sellerId)) > 0;
        }
        String hint = (sellerId != null && !following) ? "关注卖家可领优惠券~" : null;
        UserCoupon existing = userCouponMapper.selectOne(
                Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, uid)
                        .eq(UserCoupon::getProductId, productId)
                        .eq(UserCoupon::getStatus, COUPON_UNUSED)
                        .gt(UserCoupon::getExpireTime, LocalDateTime.now())
                        .last("LIMIT 1"));
        Long couponId = existing != null ? existing.getId() : null;
        long claimedOrUsedCount = userCouponMapper.selectCount(
                Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, uid)
                        .eq(UserCoupon::getProductId, productId));
        boolean claimable = sellerId != null && following && claimedOrUsedCount == 0;
        long discountCent = 0L;
        if (existing != null) {
            List<LineAgg> aggs = List.of(new LineAgg(productId, quantity, unit));
            discountCent = computeCouponDiscount(existing, totalCent, aggs);
        }
        userWalletService.ensureWallet(uid);
        long bal = userWalletService.getBalanceCent(uid);
        long payCent = totalCent - discountCent;
        DirectBuyPreviewVO vo = DirectBuyPreviewVO.builder()
                .productId(productId)
                .title(p.getTitle())
                .cover(p.getCover())
                .priceCent(unit)
                .quantity(quantity)
                .sellerId(sellerId)
                .followingSeller(following)
                .followSellerCouponHint(hint)
                .claimableFollowCoupon(claimable)
                .userCouponId(couponId)
                .totalCent(totalCent)
                .discountCent(discountCent)
                .payCent(payCent)
                .walletBalanceCent(bal)
                .build();
        return Result.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createDirect(OrderCreateDirectDTO dto) {
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            return Result.fail("数量无效");
        }
        Long uid = UserHolder.getUser().getId();
        Product p = productService.getById(dto.getProductId());
        if (p == null || !Objects.equals(p.getStatus(), PRODUCT_ONLINE)) {
            return Result.fail("商品不可用");
        }
        int q = dto.getQuantity();
        if (p.getStock() == null || p.getStock() < q) {
            return Result.fail("库存不足");
        }
        long unit = p.getPriceCent() != null ? p.getPriceCent() : 0L;
        LineAgg agg = new LineAgg(p.getId(), q, unit);
        List<LineAgg> aggs = List.of(agg);
        long totalCent = agg.subtotal();

        long discountCent = 0L;
        Long userCouponId = null;
        if (dto.getUserCouponId() != null) {
            UserCoupon c = userCouponMapper.selectById(dto.getUserCouponId());
            if (c == null || !Objects.equals(c.getUserId(), uid)) {
                throw new BizException("优惠券不存在");
            }
            discountCent = computeCouponDiscount(c, totalCent, aggs);
            userCouponId = c.getId();
        }

        long payCent = totalCent - discountCent;

        ShopOrder order = new ShopOrder();
        order.setUserId(uid);
        order.setTotalCent(totalCent);
        order.setPayCent(payCent);
        order.setDiscountCent(discountCent);
        order.setUserCouponId(userCouponId);
        order.setStatus(ORDER_CREATED);
        order.setFromCart(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.insert(order);

        OrderItem oi = new OrderItem();
        oi.setOrderId(order.getId());
        oi.setProductId(p.getId());
        oi.setQuantity(q);
        oi.setPriceCent(p.getPriceCent());
        orderItemMapper.insert(oi);
        return Result.ok(order.getId());
    }

    @Override
    public Result my(long current, long size) {
        Long uid = UserHolder.getUser().getId();
        Page<ShopOrder> page = Page.of(current, size);
        var p = shopOrderMapper.selectPage(page,
                Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getUserId, uid).orderByDesc(ShopOrder::getCreateTime));
        List<ShopOrder> records = p.getRecords();
        if (records.isEmpty()) {
            return Result.ok(List.of(), p.getTotal());
        }
        List<Long> orderIds = records.stream().map(ShopOrder::getId).toList();
        List<OrderItem> allItems = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery()
                        .in(OrderItem::getOrderId, orderIds)
                        .orderByAsc(OrderItem::getId));
        Map<Long, List<OrderItem>> byOrder = new LinkedHashMap<>();
        for (Long oid : orderIds) {
            byOrder.put(oid, new ArrayList<>());
        }
        for (OrderItem oi : allItems) {
            List<OrderItem> lst = byOrder.get(oi.getOrderId());
            if (lst != null) {
                lst.add(oi);
            }
        }
        List<Long> productIds = allItems.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Product> productMap = productIds.isEmpty()
                ? Map.of()
                : productService.listByIds(productIds).stream()
                        .filter(Objects::nonNull)
                        .filter(pr -> pr.getId() != null)
                        .collect(Collectors.toMap(Product::getId, pr -> pr, (a, b) -> a));

        List<OrderListRowVO> rows = new ArrayList<>();
        for (ShopOrder order : records) {
            List<OrderItem> items = byOrder.getOrDefault(order.getId(), List.of());
            List<OrderListProductPreviewVO> previews = new ArrayList<>();
            String listTitle;
            if (items.isEmpty()) {
                listTitle = "订单";
            } else if (items.size() == 1) {
                OrderItem oi = items.getFirst();
                Product prod = productMap.get(oi.getProductId());
                String title = prod != null ? prod.getTitle() : ("商品#" + oi.getProductId());
                int q = oi.getQuantity() != null ? oi.getQuantity() : 0;
                listTitle = q > 1 ? title + " ×" + q : title;
                previews.add(buildOrderListPreview(oi, prod));
            } else {
                OrderItem first = items.getFirst();
                Product prod0 = productMap.get(first.getProductId());
                String firstTitle = prod0 != null ? prod0.getTitle() : ("商品#" + first.getProductId());
                int totalPieces = items.stream()
                        .mapToInt(oi -> oi.getQuantity() != null ? oi.getQuantity() : 0)
                        .sum();
                int n = totalPieces > 0 ? totalPieces : items.size();
                listTitle = firstTitle + " 等共 " + n + " 件";
                for (OrderItem oi : items) {
                    previews.add(buildOrderListPreview(oi, productMap.get(oi.getProductId())));
                }
            }
            rows.add(OrderListRowVO.builder()
                    .id(order.getId())
                    .totalCent(order.getTotalCent())
                    .payCent(order.getPayCent())
                    .discountCent(order.getDiscountCent())
                    .status(order.getStatus())
                    .sellerSettled(order.getSellerSettled())
                    .payTime(order.getPayTime())
                    .createTime(order.getCreateTime())
                    .listTitle(listTitle)
                    .products(previews)
                    .build());
        }
        return Result.ok(rows, p.getTotal());
    }

    private static OrderListProductPreviewVO buildOrderListPreview(OrderItem oi, Product prod) {
        Long pid = oi.getProductId();
        return OrderListProductPreviewVO.builder()
                .productId(pid)
                .title(prod != null ? prod.getTitle() : ("商品#" + pid))
                .cover(prod != null ? prod.getCover() : null)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result refund(Long orderId) {
        Long uid = UserHolder.getUser().getId();
        ShopOrder order = shopOrderMapper.selectOne(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getId, orderId)
                .last("FOR UPDATE"));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!Objects.equals(order.getUserId(), uid)) {
            throw new BizException("无权操作该订单");
        }
        if (!Objects.equals(order.getStatus(), ORDER_PAID)) {
            throw new BizException("订单不可退款");
        }
        if (order.getPayCent() == null) {
            throw new BizException("该订单不支持钱包退款");
        }
        if (Objects.equals(order.getSellerSettled(), 1)) {
            throw new BizException("已超过退款期限，货款已结算给卖家");
        }
        if (order.getPayTime() == null) {
            throw new BizException("订单支付信息异常，无法退款");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(order.getPayTime().plusHours(REFUND_WINDOW_HOURS))) {
            throw new BizException("支付已超过1小时，无法退款");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
        for (OrderItem oi : items) {
            Product p = productService.getById(oi.getProductId());
            if (p != null) {
                int qty = oi.getQuantity() != null ? oi.getQuantity() : 0;
                p.setStock((p.getStock() != null ? p.getStock() : 0) + qty);
                p.setUpdateTime(LocalDateTime.now());
                productService.updateById(p);
            }
        }

        long payBack = order.getPayCent() != null ? order.getPayCent() : 0L;
        userWalletService.addBalance(uid, payBack);

        if (order.getUserCouponId() != null) {
            userCouponMapper.update(null,
                    Wrappers.<UserCoupon>lambdaUpdate()
                            .set(UserCoupon::getStatus, COUPON_UNUSED)
                            .eq(UserCoupon::getId, order.getUserCouponId())
                            .eq(UserCoupon::getUserId, uid)
                            .eq(UserCoupon::getStatus, COUPON_USED));
        }

        order.setStatus(ORDER_REFUNDED);
        order.setUpdateTime(now);
        shopOrderMapper.updateById(order);
        return Result.ok();
    }

    private boolean withinPayWindow(ShopOrder order) {
        if (order == null || order.getCreateTime() == null) {
            return false;
        }
        return !order.getCreateTime().plusMinutes(PAY_TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
    }

    /**
     * 购物车订单支付成功：按订单明细从购物车扣减件数（无行或件数已变则尽量扣减，避免误动「立即购买」订单的购物车）。
     */
    private void removePaidOrderItemsFromCart(Long userId, Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
        LocalDateTime now = LocalDateTime.now();
        for (OrderItem oi : items) {
            int paidQ = oi.getQuantity() != null ? oi.getQuantity() : 0;
            if (paidQ <= 0) {
                continue;
            }
            Cart ex = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getUserId, userId)
                    .eq(Cart::getProductId, oi.getProductId())
                    .last("LIMIT 1"));
            if (ex == null) {
                continue;
            }
            int cartQ = ex.getQuantity() != null ? ex.getQuantity() : 0;
            int newQ = cartQ - paidQ;
            if (newQ <= 0) {
                cartMapper.deleteById(ex.getId());
            } else {
                ex.setQuantity(newQ);
                ex.setUpdateTime(now);
                cartMapper.updateById(ex);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result pay(Long orderId, String payPassword) {
        if (payPassword == null || !MOCK_PAY_PASSWORD.equals(payPassword.trim())) {
            throw new BizException("支付密码错误");
        }
        Long uid = UserHolder.getUser().getId();
        ShopOrder order = shopOrderMapper.selectOne(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getId, orderId)
                .last("FOR UPDATE"));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!Objects.equals(order.getUserId(), uid)) {
            throw new BizException("无权操作该订单");
        }
        if (!Objects.equals(order.getStatus(), ORDER_CREATED)) {
            throw new BizException("订单不可支付");
        }
        if (!withinPayWindow(order)) {
            throw new BizException("订单已超时，请重新下单或查看是否已自动取消");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            throw new BizException("订单明细为空");
        }
        for (OrderItem oi : items) {
            Product prod = productService.getById(oi.getProductId());
            if (prod == null || !Objects.equals(prod.getStatus(), PRODUCT_ONLINE)) {
                throw new BizException("商品不可用");
            }
            int need = oi.getQuantity() != null ? oi.getQuantity() : 0;
            if (prod.getStock() == null || prod.getStock() < need) {
                throw new BizException("库存不足");
            }
        }

        long payCent = order.getPayCent() != null ? order.getPayCent() : 0L;
        if (!userWalletService.tryDeduct(uid, payCent)) {
            throw new BizException("钱包余额不足");
        }

        if (order.getUserCouponId() != null) {
            int n = userCouponMapper.update(null,
                    Wrappers.<UserCoupon>lambdaUpdate()
                            .set(UserCoupon::getStatus, COUPON_USED)
                            .eq(UserCoupon::getId, order.getUserCouponId())
                            .eq(UserCoupon::getUserId, uid)
                            .eq(UserCoupon::getStatus, COUPON_UNUSED));
            if (n != 1) {
                userWalletService.addBalance(uid, payCent);
                throw new BizException("优惠券不可用");
            }
        }

        for (OrderItem oi : items) {
            Product prod = productService.getById(oi.getProductId());
            int need = oi.getQuantity() != null ? oi.getQuantity() : 0;
            prod.setStock(prod.getStock() - need);
            prod.setUpdateTime(LocalDateTime.now());
            productService.updateById(prod);
        }

        LocalDateTime paidAt = LocalDateTime.now();
        order.setStatus(ORDER_PAID);
        order.setPayTime(paidAt);
        order.setSellerSettled(0);
        order.setUpdateTime(paidAt);
        shopOrderMapper.updateById(order);
        if (Objects.equals(order.getFromCart(), 1)) {
            removePaidOrderItemsFromCart(uid, orderId);
        }
        return Result.ok(order.getId());
    }

    @Override
    public Result detail(Long orderId) {
        Long uid = UserHolder.getUser().getId();
        ShopOrder order = shopOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!Objects.equals(order.getUserId(), uid)) {
            throw new BizException("无权查看该订单");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
        List<OrderLineVO> lines = new ArrayList<>();
        for (OrderItem oi : items) {
            Product prod = productService.getById(oi.getProductId());
            String title = prod != null ? prod.getTitle() : ("商品#" + oi.getProductId());
            String cover = prod != null ? prod.getCover() : null;
            lines.add(OrderLineVO.builder()
                    .productId(oi.getProductId())
                    .title(title)
                    .cover(cover)
                    .quantity(oi.getQuantity())
                    .priceCent(oi.getPriceCent())
                    .build());
        }
        boolean payable = Objects.equals(order.getStatus(), ORDER_CREATED) && withinPayWindow(order);
        LocalDateTime deadline = order.getCreateTime() != null
                ? order.getCreateTime().plusMinutes(PAY_TIMEOUT_MINUTES)
                : null;
        LocalDateTime payTime = order.getPayTime();
        LocalDateTime refundDeadline = payTime != null ? payTime.plusHours(REFUND_WINDOW_HOURS) : null;
        boolean refundable = Objects.equals(order.getStatus(), ORDER_PAID)
                && !Objects.equals(order.getSellerSettled(), 1)
                && order.getPayCent() != null
                && payTime != null
                && LocalDateTime.now().isBefore(payTime.plusHours(REFUND_WINDOW_HOURS));
        OrderDetailVO vo = OrderDetailVO.builder()
                .id(order.getId())
                .status(order.getStatus())
                .totalCent(order.getTotalCent())
                .payCent(order.getPayCent())
                .discountCent(order.getDiscountCent())
                .userCouponId(order.getUserCouponId())
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime())
                .payTime(payTime)
                .sellerSettled(order.getSellerSettled())
                .refundable(refundable)
                .refundDeadline(Objects.equals(order.getStatus(), ORDER_PAID) ? refundDeadline : null)
                .payable(payable)
                .payDeadline(Objects.equals(order.getStatus(), ORDER_CREATED) ? deadline : null)
                .items(lines)
                .build();
        return Result.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelExpiredPendingOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(PAY_TIMEOUT_MINUTES);
        List<ShopOrder> list = shopOrderMapper.selectList(
                Wrappers.<ShopOrder>lambdaQuery()
                        .eq(ShopOrder::getStatus, ORDER_CREATED)
                        .lt(ShopOrder::getCreateTime, deadline));
        if (list.isEmpty()) {
            return 0;
        }
        for (ShopOrder o : list) {
            o.setStatus(ORDER_CANCELLED);
            o.setUpdateTime(LocalDateTime.now());
            shopOrderMapper.updateById(o);
        }
        return list.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result close(Long orderId) {
        Long uid = UserHolder.getUser().getId();
        ShopOrder order = shopOrderMapper.selectOne(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getId, orderId)
                .last("FOR UPDATE"));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!Objects.equals(order.getUserId(), uid)) {
            throw new BizException("无权操作该订单");
        }
        if (!Objects.equals(order.getStatus(), ORDER_CREATED)) {
            throw new BizException("仅待支付订单可关闭");
        }
        order.setStatus(ORDER_CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteRecord(Long orderId) {
        Long uid = UserHolder.getUser().getId();
        ShopOrder order = shopOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!Objects.equals(order.getUserId(), uid)) {
            throw new BizException("无权操作该订单");
        }
        if (Objects.equals(order.getStatus(), ORDER_CREATED)) {
            throw new BizException("请先关闭待支付订单后再删除记录");
        }
        shopOrderMapper.deleteById(orderId);
        return Result.ok();
    }

    @Override
    public int settlePaidOrdersToSellers() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(REFUND_WINDOW_HOURS);
        List<ShopOrder> candidates = shopOrderMapper.selectList(
                Wrappers.<ShopOrder>lambdaQuery()
                        .eq(ShopOrder::getStatus, ORDER_PAID)
                        .eq(ShopOrder::getSellerSettled, 0)
                        .isNotNull(ShopOrder::getPayTime)
                        .lt(ShopOrder::getPayTime, threshold));
        int done = 0;
        for (ShopOrder o : candidates) {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus st = transactionManager.getTransaction(def);
            try {
                if (settleOnePaidOrder(o.getId())) {
                    done++;
                }
                transactionManager.commit(st);
            } catch (Exception e) {
                transactionManager.rollback(st);
                log.warn("订单结算失败 id={}", o.getId(), e);
            }
        }
        return done;
    }

    /**
     * 在已开启的事务内执行；须由调用方对订单行 FOR UPDATE 后调用本方法逻辑，或在本方法内加锁。
     */
    private boolean settleOnePaidOrder(Long orderId) {
        ShopOrder order = shopOrderMapper.selectOne(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getId, orderId)
                .last("FOR UPDATE"));
        if (order == null || !Objects.equals(order.getStatus(), ORDER_PAID)
                || !Objects.equals(order.getSellerSettled(), 0)
                || order.getPayTime() == null) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().minusHours(REFUND_WINDOW_HOURS);
        if (!order.getPayTime().isBefore(threshold)) {
            return false;
        }
        long payCent = order.getPayCent() != null ? order.getPayCent() : 0L;
        if (payCent <= 0) {
            order.setSellerSettled(1);
            order.setUpdateTime(LocalDateTime.now());
            shopOrderMapper.updateById(order);
            return true;
        }
        Map<Long, Long> subBySeller = new LinkedHashMap<>();
        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
        for (OrderItem oi : items) {
            Product p = productService.getById(oi.getProductId());
            if (p == null || p.getSellerId() == null) {
                continue;
            }
            long pc = oi.getPriceCent() != null ? oi.getPriceCent() : 0L;
            int q = oi.getQuantity() != null ? oi.getQuantity() : 0;
            subBySeller.merge(p.getSellerId(), pc * q, Long::sum);
        }
        long sumEligible = subBySeller.values().stream().mapToLong(Long::longValue).sum();
        if (sumEligible <= 0) {
            log.warn("订单实付无法结算：明细无卖家 orderId={} payCent={}", orderId, payCent);
            return false;
        }
        List<Map.Entry<Long, Long>> entries = new ArrayList<>(subBySeller.entrySet());
        long allocated = 0L;
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Long, Long> e = entries.get(i);
            long share = (i == entries.size() - 1)
                    ? (payCent - allocated)
                    : (payCent * e.getValue() / sumEligible);
            if (i < entries.size() - 1) {
                allocated += share;
            }
            if (share > 0) {
                userWalletService.addBalance(e.getKey(), share);
            }
        }
        order.setSellerSettled(1);
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);
        return true;
    }
}
