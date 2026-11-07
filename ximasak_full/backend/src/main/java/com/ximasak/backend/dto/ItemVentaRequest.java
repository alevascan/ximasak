package com.ximasak.backend.dto;

import lombok.Data;

@Data
public class ItemVentaRequest {
    private Integer idProducto;
    private Integer cantidad;
}