package com.aura_api.aura_farmer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequestDTO {

    @NotBlank(message = "El username/alias es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    private String username;

    private String email;

    @NotNull(message = "Debes indicar cuánto porcentaje de Aura querés comprar")
    @Min(value = 1, message = "La compra mínima es 1% de Aura")
    private Long auraAmount;

    @Size(max = 120, message = "La frase no puede superar los 120 caracteres")
    private String customPhrase;
}