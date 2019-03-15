/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.intern.ble_ex1;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes
{
    private static HashMap<String, String> attributes = new HashMap();
    //Service and Characteristic by type
    static String SERVICE_BODY_TEMPERATURE = "8AAD8CD4-3830-45EE-A13E-74F0B01013CE";

    static String TEMPERATURE = "8AAD46AA-3830-45EE-A13E-74F0B01013CE";
    static String TEMPERATURE_CCC = "00002902-0000-1000-8000-00805f9b34fb";
    static String SAMPLING_RATE = "8AAD46AB-3830-45EE-A13E-74F0B01013CE";

    static
    {
        //Services
        attributes.put(SERVICE_BODY_TEMPERATURE, "Body Temperature Service");       //Body Temperature Service
        //Characteristics
        attributes.put(TEMPERATURE, "Body Temperature Measurement");        //Body Temperature Measurement for Body Temperature Service
        attributes.put(SAMPLING_RATE, "Sampling Rate");                     //Sampling Rate for Configuration Service
        //Client Characteristic Configuration
        attributes.put(TEMPERATURE_CCC, "Body Temperature Client Configuration");        //Body Temperature Client Configuration for Body Temperature Service
    }

    public static String lookup(String uuid, String defaultName)
    {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
