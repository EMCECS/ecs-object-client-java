/*
 * Copyright 2013 EMC Corporation. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
@XmlSchema(namespace = "http://s3.amazonaws.com/doc/2006-03-01/", elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED)
@XmlJavaTypeAdapter(value = com.emc.object.util.Iso8601Adapter.class, type = java.util.Date.class) package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;