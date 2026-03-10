package org.adso.minimarket.constant;

public class ProductRoutes {
    //publicas
    public static final String GET_PRODUCT = "/products/{id}";
    public static final String GET_FEATURED_PRODUCTS = "/products/featured";

    //privadas
    public static final String CREATE_PRODUCT = "/products";
    public static final String UPDATE_PRODUCT = "/products/{id}";
    public static final String DELETE_PRODUCT = "/products/{id}";
}
