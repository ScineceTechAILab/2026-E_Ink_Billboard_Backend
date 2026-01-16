package com.stalab.e_ink_billboard_backend.common;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 响应对象
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 7000723935764546321L;
    private Integer code;
    private String info;
    private T data;

}
