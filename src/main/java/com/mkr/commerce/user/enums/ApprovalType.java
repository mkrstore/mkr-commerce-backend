package com.mkr.commerce.user.enums;

public enum ApprovalType {
    EDIT_PROFILE,   // field change that needs approval (e.g. email change by ADMIN)
    ROLE_CHANGE,    // reserved for future approval-queue implementation
    ACCOUNT_STATUS  // reserved for future approval-queue implementation
}
