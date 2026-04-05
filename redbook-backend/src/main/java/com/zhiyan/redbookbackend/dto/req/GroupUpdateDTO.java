package com.zhiyan.redbookbackend.dto.req;

import lombok.Data;

/**
 * 群主更新群资料。字段为 null 表示不修改；avatar 传空字符串表示清除自定义头像。
 */
@Data
public class GroupUpdateDTO {
    private String name;
    /** 群头像 URL；null 不改，空字符串清除自定义 */
    private String avatar;
}
