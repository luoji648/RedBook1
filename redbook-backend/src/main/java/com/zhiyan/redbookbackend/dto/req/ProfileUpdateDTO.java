package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新昵称与头像")
public class ProfileUpdateDTO {
    private String nickName;
    private String icon;
}
