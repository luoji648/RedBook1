package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyan.redbookbackend.entity.Product;
import com.zhiyan.redbookbackend.mapper.ProductMapper;
import com.zhiyan.redbookbackend.service.IProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {
}
