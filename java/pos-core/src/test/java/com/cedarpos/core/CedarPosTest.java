package com.cedarpos.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CedarPosTest {

    @Test
    void productNameIsSet() {
        assertThat(CedarPos.PRODUCT_NAME).isEqualTo("Cedar POS");
    }
}
