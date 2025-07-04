package com.dev.IbioScience.repository.product.register;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.Manufacturer;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {}

