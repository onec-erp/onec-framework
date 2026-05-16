package com.onec.fixtures;

import com.onec.annotations.Attribute;
import com.onec.annotations.Dimension;
import com.onec.annotations.InformationRegister;
import com.onec.model.InformationRecord;
import com.onec.model.Periodicity;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@InformationRegister(name = "Settings", periodicity = Periodicity.NONE)
public class TestSettingRegister extends InformationRecord {

    @Dimension
    private UUID userId;

    @Attribute(length = 255)
    private String settingValue;
}
