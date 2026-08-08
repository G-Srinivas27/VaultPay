package com.srinivas.vaultpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VaultpayApplication {

	public static void main(String[] args) {
		SpringApplication.run(VaultpayApplication.class, args);
	}

}
