package com.dev.IbioScience.dto.front.productDetail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPermissionResponse {

    private boolean loggedIn;
    private boolean purchased;
    private boolean alreadyReviewed;
    private boolean canWrite;
    private String message;
}
