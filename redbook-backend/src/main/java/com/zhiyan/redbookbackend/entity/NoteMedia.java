package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_note_media")
public class NoteMedia {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long noteId;
    private String url;
    private Integer sortOrder;
    /** 0 image 1 video */
    private Integer mediaType;
}
