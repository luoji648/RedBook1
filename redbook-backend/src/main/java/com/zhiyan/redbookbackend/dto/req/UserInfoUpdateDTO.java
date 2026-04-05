package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "用户扩展资料")
public class UserInfoUpdateDTO {
    private String city;
    private String introduce;
    private Boolean gender;
    private LocalDate birthday;
    /** 收藏列表是否对他人公开 */
    private Boolean collectPublic;
    /** 赞过列表是否对他人公开 */
    private Boolean likePublic;
}
