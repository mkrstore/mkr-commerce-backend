package com.mkr.commerce.user.enums;

/**
 * Staff roles stored as strings in the DB (not ordinal)
 * so adding new roles never corrupts existing data.
 */
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    SALES,
    INVENTORY,
    SUPPORT
}
