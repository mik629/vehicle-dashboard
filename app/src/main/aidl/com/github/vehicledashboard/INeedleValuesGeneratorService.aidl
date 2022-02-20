package com.github.vehicledashboard;

import com.github.vehicledashboard.NeedleValue;

interface INeedleValuesGeneratorService {
    List<NeedleValue> generateNextValues(
        String meterType,
        String engineMode
    );
}