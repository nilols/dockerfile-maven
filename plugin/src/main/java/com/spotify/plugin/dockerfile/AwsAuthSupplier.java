/*-
 * -\-\-
 * Dockerfile Maven Plugin
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
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
 * -/-/-
 */

package com.spotify.plugin.dockerfile;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenResult;
import com.spotify.docker.client.ImageRef;
import com.spotify.docker.client.auth.RegistryAuthSupplier;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.RegistryAuth;
import com.spotify.docker.client.messages.RegistryConfigs;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import java.util.Base64;

public class AwsAuthSupplier implements RegistryAuthSupplier {

    private final Settings settings;

    public AwsAuthSupplier(Settings settings) {
        this.settings = settings;
    }

    @Override
    public RegistryAuth authFor(String imageName) throws DockerException {
        final ImageRef reference = new ImageRef(imageName);
        final String registryName = reference.getRegistryName();
        Server server = settings.getServer(registryName);
        if (server != null) {
            AmazonECR client = AmazonECRClientBuilder.standard().withCredentials(getAWSCredentials(server)).build();
            GetAuthorizationTokenResult result = client.getAuthorizationToken(new GetAuthorizationTokenRequest());
            String token = result.getAuthorizationData().get(0).getAuthorizationToken();
            String[] decoded = new String(Base64.getDecoder().decode(token)).split(":");
            return RegistryAuth.builder()
                    .username(decoded[0])
                    .password(decoded[1])
                    .build();
        }
        return null;
    }

    private AWSCredentialsProvider getAWSCredentials(Server server) {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(server.getUsername(), server.getPassword()));
    }

    @Override
    public RegistryAuth authForSwarm() throws DockerException {
        return null;
    }

    @Override
    public RegistryConfigs authForBuild() throws DockerException {
        return null;
    }
}
