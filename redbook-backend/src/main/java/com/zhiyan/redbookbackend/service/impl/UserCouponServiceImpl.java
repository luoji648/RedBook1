package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.CouponClaimFollowDTO;
import com.zhiyan.redbookbackend.dto.resp.UserCouponVO;
import com.zhiyan.redbookbackend.entity.Product;
import com.zhiyan.redbookbackend.entity.UserCoupon;
import com.zhiyan.redbookbackend.entity.UserFollow;
import com.zhiyan.redbookbackend.mapper.UserCouponMapper;
import com.zhiyan.redbookbackend.mapper.UserFollowMapper;
import com.zhiyan.redbookbackend.service.IProductService;
import com.zhiyan.redbookbackend.service.IUserCouponService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserCouponServiceImpl implements IUserCouponService {

    private static final int COUPON_UNUSED = 0;
    private static final int COUPON_FOLLOW_EXPIRE_DAYS = 365;

    @Resource
    private UserCouponMapper userCouponMapper;
    @Resource
    private UserFollowMapper userFollowMapper;
    @Resource
    private IProductService productService;

    @Override
    public Result my(long current, long size) {
        Long uid = UserHolder.getUser().getId();
        Page<UserCoupon> page = Page.of(current, size);
        var p = userCouponMapper.selectPage(page,
                Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, uid)
                        .eq(UserCoupon::getStatus, COUPON_UNUSED)
                        .gt(UserCoupon::getExpireTime, LocalDateTime.now())
                        .orderByDesc(UserCoupon::getCreateTime));
        List<UserCouponVO> vos = toVoList(p.getRecords());
        return Result.ok(vos, p.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result claimFollowSeller(CouponClaimFollowDTO dto) {
        Long productId = dto.getProductId();
        Long uid = UserHolder.getUser().getId();
        Product p = productService.getById(productId);
        if (p == null) {
            return Result.fail("商品不存在");
        }
        Long sellerId = p.getSellerId();
        if (sellerId == null && dto.getFallbackSellerUserId() != null) {
            sellerId = dto.getFallbackSellerUserId();
        }
        if (sellerId == null) {
            return Result.fail("商品暂无卖家信息，无法领取优惠券");
        }
        if (Objects.equals(sellerId, uid)) {
            return Result.fail("不能领取自己的商品券");
        }
        long fc = userFollowMapper.selectCount(Wrappers.<UserFollow>lambdaQuery()
                .eq(UserFollow::getFollowerId, uid)
                .eq(UserFollow::getFolloweeId, sellerId));
        if (fc == 0) {
            return Result.fail("关注卖家可领优惠券~");
        }

        UserCoupon any = userCouponMapper.selectOne(
                Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, uid)
                        .eq(UserCoupon::getProductId, productId)
                        .last("LIMIT 1"));
        if (any != null) {
            if (Objects.equals(any.getStatus(), COUPON_UNUSED) && any.getExpireTime().isAfter(LocalDateTime.now())) {
                return Result.ok(any.getId());
            }
            return Result.fail("每种商品仅可领取一次");
        }

        long unitPrice = p.getPriceCent() != null ? p.getPriceCent() : 0L;
        long discountCent = unitPrice / 10L;
        if (discountCent <= 0L) {
            return Result.fail("商品价格过低，无法生成优惠券");
        }

        LocalDateTime now = LocalDateTime.now();
        UserCoupon uc = new UserCoupon();
        uc.setUserId(uid);
        uc.setProductId(productId);
        uc.setTitle("关注卖家券 · " + truncate(p.getTitle(), 64));
        uc.setDiscountCent(discountCent);
        uc.setMinOrderCent(unitPrice);
        uc.setStatus(COUPON_UNUSED);
        uc.setExpireTime(now.plusDays(COUPON_FOLLOW_EXPIRE_DAYS));
        uc.setCreateTime(now);
        userCouponMapper.insert(uc);
        return Result.ok(uc.getId());
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private List<UserCouponVO> toVoList(List<UserCoupon> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Set<Long> pids = records.stream()
                .map(UserCoupon::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Product> pmap = pids.isEmpty()
                ? Map.of()
                : productService.listByIds(pids).stream().collect(Collectors.toMap(Product::getId, x -> x, (a, b) -> a));
        List<UserCouponVO> vos = new ArrayList<>(records.size());
        for (UserCoupon c : records) {
            UserCouponVO v = new UserCouponVO();
            v.setId(c.getId());
            v.setUserId(c.getUserId());
            v.setProductId(c.getProductId());
            v.setTitle(c.getTitle());
            v.setDiscountCent(c.getDiscountCent());
            v.setMinOrderCent(c.getMinOrderCent());
            v.setStatus(c.getStatus());
            v.setExpireTime(c.getExpireTime());
            v.setCreateTime(c.getCreateTime());
            if (c.getProductId() != null) {
                Product pr = pmap.get(c.getProductId());
                if (pr != null) {
                    v.setProductTitle(pr.getTitle());
                    v.setProductCover(pr.getCover());
                }
            }
            vos.add(v);
        }
        return vos;
    }
}
