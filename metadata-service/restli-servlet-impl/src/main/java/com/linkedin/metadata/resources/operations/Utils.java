package com.linkedin.metadata.resources.operations;

import static com.datahub.authorization.AuthUtil.isAPIAuthorized;

import com.datahub.authentication.Authentication;
import com.datahub.authentication.AuthenticationContext;
import com.datahub.authorization.EntitySpec;
import com.datahub.plugins.auth.authorization.Authorizer;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.metadata.authorization.PoliciesConfig;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.entity.restoreindices.RestoreIndicesArgs;
import com.linkedin.metadata.entity.restoreindices.RestoreIndicesResult;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.RestLiServiceException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.datahubproject.metadata.context.OperationContext;
import io.datahubproject.metadata.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class Utils {

  private Utils() {}

  public static String restoreIndices(
          @Nonnull OperationContext systemOperationContext,
      @Nonnull String aspectName,
      @Nullable String urn,
      @Nullable String urnLike,
      @Nullable Integer start,
      @Nullable Integer batchSize,
      @Nullable Integer limit,
      @Nullable Long gePitEpochMs,
      @Nullable Long lePitEpochMs,
      @Nonnull Authorizer authorizer,
      @Nonnull EntityService<?> entityService) {

    EntitySpec resourceSpec = null;
    if (StringUtils.isNotBlank(urn)) {
      Urn resource = UrnUtils.getUrn(urn);
      resourceSpec = new EntitySpec(resource.getEntityType(), resource.toString());
    }
    final Authentication auth = AuthenticationContext.getAuthentication();
    if (!isAPIAuthorized(
            auth,
            authorizer,
            PoliciesConfig.RESTORE_INDICES_PRIVILEGE,
            resourceSpec)) {
      throw new RestLiServiceException(
          HttpStatus.S_403_FORBIDDEN, "User is unauthorized to restore indices.");
    }
    final OperationContext opContext = OperationContext.asSession(
            systemOperationContext, RequestContext.builder().buildRestli("restoreIndices", List.of()), authorizer, auth, true);

    RestoreIndicesArgs args =
        new RestoreIndicesArgs()
            .aspectName(aspectName)
            .urnLike(urnLike)
            .urn(urn)
            .start(start)
            .batchSize(batchSize)
            .limit(limit)
            .gePitEpochMs(gePitEpochMs)
            .lePitEpochMs(lePitEpochMs);
    Map<String, Object> result = new HashMap<>();
    result.put("args", args);
    result.put("result", entityService
            .streamRestoreIndices(opContext, args, log::info)
            .map(RestoreIndicesResult::toString)
            .collect(Collectors.joining("\n")));
    return result.toString();
  }
}
