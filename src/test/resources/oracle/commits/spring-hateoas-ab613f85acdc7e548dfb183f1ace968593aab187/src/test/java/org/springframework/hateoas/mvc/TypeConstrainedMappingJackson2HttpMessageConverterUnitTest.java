/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.hateoas.mvc;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Unit tests for {@link TypeConstrainedMappingJackson2HttpMessageConverter}.
 * 
 * @author Oliver Gierke
 */
public class TypeConstrainedMappingJackson2HttpMessageConverterUnitTest {

	/**
	 * @see #219
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullType() {
		new TypeConstrainedMappingJackson2HttpMessageConverter(null);
	}

	/**
	 * @see #219
	 */
	@Test
	public void canReadTypeIfAssignableToConfiguredType() {

		HttpMessageConverter<Object> converter = new TypeConstrainedMappingJackson2HttpMessageConverter(
				ResourceSupport.class);

		assertThat(converter.canRead(Object.class, MediaType.APPLICATION_JSON), is(false));
		assertThat(converter.canRead(ResourceSupport.class, MediaType.APPLICATION_JSON), is(true));
		assertThat(converter.canRead(Resource.class, MediaType.APPLICATION_JSON), is(true));
	}

	/**
	 * @see #219
	 */
	@Test
	public void canWriteTypeIfAssignableToConfiguredType() {

		HttpMessageConverter<Object> converter = new TypeConstrainedMappingJackson2HttpMessageConverter(
				ResourceSupport.class);

		assertThat(converter.canWrite(Object.class, MediaType.APPLICATION_JSON), is(false));
		assertThat(converter.canWrite(ResourceSupport.class, MediaType.APPLICATION_JSON), is(true));
		assertThat(converter.canWrite(Resource.class, MediaType.APPLICATION_JSON), is(true));
	}

	/**
	 * @see #360
	 */
	@Test
	public void doesNotSupportAnythingButTheConfiguredClassForCanReadWithContextClass() {

		GenericHttpMessageConverter<Object> converter = new TypeConstrainedMappingJackson2HttpMessageConverter(
				ResourceSupport.class);

		assertThat(converter.canRead(String.class, Object.class, MediaType.APPLICATION_JSON), is(false));
	}
}
