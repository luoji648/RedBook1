package com.zhiyan.redbookbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.ProductSaveDTO;
import com.zhiyan.redbookbackend.dto.req.ProductStockUpdateDTO;
import com.zhiyan.redbookbackend.entity.Product;

public interface IProductService extends IService<Product> {

    /**
     * 新建或更新商品（元数据；封面图需先走 OSS 上传拿到 URL）。
     */
    Result saveFromDto(ProductSaveDTO dto);

    /**
     * 卖家调整已上架商品的库存（仅更新 stock、updateTime）。
     */
    Result updatePublishedStock(Long productId, ProductStockUpdateDTO dto);
}
