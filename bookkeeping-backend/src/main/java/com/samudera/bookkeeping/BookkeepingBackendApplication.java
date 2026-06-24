package com.samudera.bookkeeping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.samudera.bookkeeping.mapper")
public class BookkeepingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookkeepingBackendApplication.class, args);
	}

}
