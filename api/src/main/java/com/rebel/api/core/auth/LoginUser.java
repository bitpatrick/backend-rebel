package com.rebel.api.core.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginUser(

	    @NotBlank(message = "L'email è obbligatoria")
	    @Email(message = "L'email deve avere un formato valido")
	    String email,

	    @NotBlank(message = "La password è obbligatoria")
	    String password

	) {
	}