
package com.linkedin.datastream.common;

import javax.annotation.Generated;
import com.linkedin.datastream.common.Datastream;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.base.CreateRequestBuilderBase;
import com.linkedin.restli.common.ResourceSpec;

@Generated(value = "com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator", comments = "LinkedIn Request Builder", date = "Thu Nov 19 16:41:26 PST 2015")
public class DatastreamCreateBuilder
    extends CreateRequestBuilderBase<String, Datastream, DatastreamCreateBuilder>
{


    public DatastreamCreateBuilder(String baseUriTemplate, ResourceSpec resourceSpec, RestliRequestOptions requestOptions) {
        super(baseUriTemplate, Datastream.class, resourceSpec, requestOptions);
    }

}