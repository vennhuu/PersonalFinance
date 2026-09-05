package com.vennhuu.PersonalFinance.Entity.Response.File;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResUploadFileDTO {
    private String fileName;
    private Instant uploadedAt;
}
