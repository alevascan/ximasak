package com.ximasak.backend.dto;


import lombok.Data;

import java.util.List;

@Data
public class VentaRequest {
    private Integer idCliente;
    private Integer idUsuario;
    private Integer idMedioPago;
    private List<ItemVentaRequest> items;
}