/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.ap.basic.mgr;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;

import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * S3 contract test for {@link DocumentPayloadManagerS3} using LocalStack in a Testcontainer. This
 * is an integration test ({@code *IT.java}) that runs via the maven-failsafe-plugin and requires
 * Docker.
 *
 * @author Philip Helger
 */
public final class DocumentPayloadManagerS3IT extends AbstractDocumentPayloadManagerContractTest
{
  private static final String BUCKET = "phoss-ap-test";

  @SuppressWarnings ("resource")
  private static final LocalStackContainer LOCALSTACK = new LocalStackContainer (DockerImageName.parse ("localstack/localstack:4")).withServices (Service.S3);

  private static S3Client s_aS3Client;

  @BeforeClass
  public static void startLocalStack ()
  {
    LOCALSTACK.start ();

    s_aS3Client = S3Client.builder ()
                          .endpointOverride (LOCALSTACK.getEndpointOverride (Service.S3))
                          .region (Region.of (LOCALSTACK.getRegion ()))
                          .credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create (LOCALSTACK.getAccessKey (),
                                                                                                              LOCALSTACK.getSecretKey ())))
                          .serviceConfiguration (S3Configuration.builder ()
                                                                .pathStyleAccessEnabled (Boolean.TRUE)
                                                                .build ())
                          .build ();

    // Create the test bucket
    s_aS3Client.createBucket (CreateBucketRequest.builder ().bucket (BUCKET).build ());
  }

  @AfterClass
  public static void stopLocalStack ()
  {
    if (s_aS3Client != null)
      s_aS3Client.close ();
    LOCALSTACK.stop ();
  }

  @Override
  protected IDocumentPayloadManager createManager ()
  {
    return new DocumentPayloadManagerS3 (s_aS3Client, BUCKET);
  }

  @Override
  protected String getBaseDir ()
  {
    return "outbound";
  }

  @Override
  protected String getNonExistentPath ()
  {
    return "s3://" + BUCKET + "/does-not-exist.xml";
  }

  @Override
  protected String extractDirectory (final String sPath)
  {
    // For S3 URIs like s3://bucket/outbound/2026/03/18/14/uuid.tmp
    // return "outbound/2026/03/18/14" (everything between bucket/ and filename)
    final String sWithoutPrefix = sPath.substring (DocumentPayloadManagerS3.S3_URI_PREFIX.length ());
    final int nFirstSlash = sWithoutPrefix.indexOf ('/');
    final String sKey = sWithoutPrefix.substring (nFirstSlash + 1);
    final int nLastSlash = sKey.lastIndexOf ('/');
    return nLastSlash >= 0 ? sKey.substring (0, nLastSlash) : "";
  }
}
