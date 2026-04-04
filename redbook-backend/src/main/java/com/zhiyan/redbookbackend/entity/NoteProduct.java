package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_note_product")
public class NoteProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long noteId;
    private Long productId;
    private Integer sortOrder;
}
