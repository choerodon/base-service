package io.choerodon.base.app.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.stereotype.Service;

import io.choerodon.asgard.schedule.annotation.JobTask;
import io.choerodon.base.app.service.AccessTokenService;
import io.choerodon.base.infra.asserts.DetailsHelperAssert;
import io.choerodon.base.infra.asserts.UserAssertHelper;
import io.choerodon.base.infra.dto.AccessTokenDTO;
import io.choerodon.base.infra.dto.UserDTO;
import io.choerodon.base.infra.feign.OauthTokenFeignClient;
import io.choerodon.base.infra.mapper.AccessTokenMapper;
import io.choerodon.base.infra.mapper.RefreshTokenMapper;
import io.choerodon.base.infra.utils.PageUtils;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.core.oauth.CustomUserDetails;

/**
 * @author Eugen
 **/
@Service
public class AccessTokenServiceImpl implements AccessTokenService {
    private static final Logger logger = LoggerFactory.getLogger(AccessTokenServiceImpl.class);
    private AccessTokenMapper accessTokenMapper;
    private RefreshTokenMapper refreshTokenMapper;
    private OauthTokenFeignClient oauthTokenFeignClient;
    private UserAssertHelper userAssertHelper;

    public AccessTokenServiceImpl(AccessTokenMapper accessTokenMapper, RefreshTokenMapper refreshTokenMapper,
                                  OauthTokenFeignClient oauthTokenFeignClient,
                                  UserAssertHelper userAssertHelper) {
        this.accessTokenMapper = accessTokenMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.oauthTokenFeignClient = oauthTokenFeignClient;
        this.userAssertHelper = userAssertHelper;
    }

    @Override
    public PageInfo<AccessTokenDTO> pagedSearch(Pageable pageable, String clientName, String currentToken, String params) {
        CustomUserDetails userDetails = DetailsHelperAssert.userDetailNotExisted();
        UserDTO userDTO = userAssertHelper.userNotExisted(userDetails.getUserId());
        List<AccessTokenDTO> result = searchAndOrderBy(clientName, currentToken, userDTO.getLoginName(), params);
        return doPage(pageable, result);
    }

    @Override
    public void delete(String tokenId, String currentToken) {
        AccessTokenDTO accessTokenDTO = accessTokenMapper.selectByPrimaryKey(tokenId);
        if (accessTokenDTO == null) {
            throw new CommonException("error.token.not.exist");
        }
        if (((DefaultOAuth2AccessToken) SerializationUtils.deserialize(accessTokenDTO.getToken())).getValue().equalsIgnoreCase(currentToken)) {
            throw new CommonException("error.delete.current.token");
        }
        oauthTokenFeignClient.deleteToken(tokenId);
        logger.info("iam delete token,tokenId:{}", tokenId);
    }

    @Override
    public void deleteList(List<String> tokenIds, String currentToken) {
        List<AccessTokenDTO> accessTokens = accessTokenMapper.selectTokenList(tokenIds);
        List<String> tokens = accessTokens.stream().map(t -> ((DefaultOAuth2AccessToken) SerializationUtils.deserialize(t.getToken())).getValue()).collect(Collectors.toList());

        if (!tokens.isEmpty() && tokens.contains(currentToken)) {
            throw new CommonException("error.delete.current.token");
        }
        if (tokens.size() != tokenIds.size()) {
            tokenIds = accessTokens.stream().map(AccessTokenDTO::getTokenId).collect(Collectors.toList());
        }
        oauthTokenFeignClient.deleteTokenList(tokenIds);
    }

    @JobTask(maxRetryCount = 2, code = "deleteAllExpiredToken", level = ResourceLevel.SITE, description = "删除所有失效token")
    @Override
    public void deleteAllExpiredToken(Map<String, Object> map) {
        List<AccessTokenDTO> accessTokens = accessTokenMapper.selectAll();
        //过滤出所有失效token
        List<AccessTokenDTO> allExpired = accessTokens.stream().filter(t -> ((DefaultOAuth2AccessToken) SerializationUtils.deserialize(t.getToken())).isExpired()).collect(Collectors.toList());
        allExpired.forEach(t -> {
            accessTokenMapper.deleteByPrimaryKey(t.getTokenId());
            refreshTokenMapper.deleteByPrimaryKey(t.getRefreshToken());
        });
        logger.info("All expired tokens have been cleared.");
    }

    private PageInfo<AccessTokenDTO> doPage(Pageable pageable, List<AccessTokenDTO> result) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        try (Page<AccessTokenDTO> pageResult = new Page<>(page, size)) {
            int total = result.size();
            pageResult.setTotal(total);
            if (size == 0) {
                pageResult.addAll(result);
            } else {
                int start = PageUtils.getBegin(page, size);
                int end = page * size > total ? total : page * size;
                pageResult.addAll(result.subList(start, end));
            }
            return pageResult.toPageInfo();
        }
    }

    private List<AccessTokenDTO> searchAndOrderBy(String clientName, String currentToken, String loginName, String params) {
        List<AccessTokenDTO> userAccessTokens = accessTokenMapper.selectTokens(loginName, clientName, params);
        List<AccessTokenDTO> result = new ArrayList<>();
        List<AccessTokenDTO> tokensWithoutCreateTime = new ArrayList<>();
        List<AccessTokenDTO> tokensWithCreateTime = new ArrayList<>();

        userAccessTokens.forEach(token -> {
            DefaultOAuth2AccessToken defaultToken = SerializationUtils.deserialize(token.getToken());
            String tokenValue = defaultToken.getValue();
            token.setAccesstoken(tokenValue);
            token.setExpirationTime(defaultToken.getExpiration());
            token.setExpire(defaultToken.isExpired());
            boolean isCurrentToken = tokenValue.equalsIgnoreCase(currentToken);
            token.setCurrentToken(isCurrentToken);
            Object createTime = defaultToken.getAdditionalInformation().get("createTime");
            if (isCurrentToken) {
                //当前token置顶
                result.add(token);
            } else {
                if (createTime == null) {
                    tokensWithoutCreateTime.add(token);
                } else {
                    token.setCreateTime((Date) createTime);
                    tokensWithCreateTime.add(token);
                }
            }
            token.setCreateTime((Date) createTime);
        });
        //有createTime的排序，没有的不排序
        tokensWithCreateTime.sort(Comparator.comparing(AccessTokenDTO::getCreateTime).reversed());
        result.addAll(tokensWithCreateTime);
        result.addAll(tokensWithoutCreateTime);
        return result;
    }
}
