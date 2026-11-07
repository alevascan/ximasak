package com.ximasak.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medios_de_pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medio_pago")
    private Integer idMedioPago;

    @Column(name = "nombre", nullable = false, length = 40)
    private String nombre;

    @Column(name = "estado")
    private Boolean estado = true;
}