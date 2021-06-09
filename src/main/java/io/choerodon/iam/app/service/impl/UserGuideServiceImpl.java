package io.choerodon.iam.app.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hzero.iam.domain.entity.Tenant;
import org.hzero.iam.infra.mapper.TenantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.iam.api.vo.UserGuideStepVO;
import io.choerodon.iam.api.vo.UserGuideVO;
import io.choerodon.iam.app.service.ProjectPermissionService;
import io.choerodon.iam.app.service.RoleMemberService;
import io.choerodon.iam.app.service.UserGuideService;
import io.choerodon.iam.infra.dto.ProjectDTO;
import io.choerodon.iam.infra.mapper.ProjectMapper;
import io.choerodon.iam.infra.mapper.UserGuideMapper;
import io.choerodon.iam.infra.mapper.UserGuideStepMapper;

/**
 * 〈功能简述〉
 * 〈〉
 *
 * @author wanghao
 * @since 2021/5/18 11:11
 */
@Service
public class UserGuideServiceImpl implements UserGuideService {

    @Value("${services.front.url:http://app.example.com}")
    private String frontUrl;
    @Autowired
    private UserGuideMapper userGuideMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private UserGuideStepMapper userGuideStepMapper;
    @Autowired
    private ProjectPermissionService projectPermissionService;
    @Autowired
    private RoleMemberService roleMemberService;

    @Override
    public UserGuideVO listUserGuideByMenuId(Long menuId, String tabCode, String guideCode, Long projectId, Long organizationId) {
        UserGuideVO userGuideVO;

        CustomUserDetails userDetails = DetailsHelper.getUserDetails();

        if (menuId != null) {
            userGuideVO = userGuideMapper.queryUserGuideByMenuIdAndCode(menuId, tabCode);
            if (userGuideVO == null) {
                List<UserGuideVO> userGuideVOList = userGuideMapper.queryUserGuideByMenuId(menuId);
                if (CollectionUtils.isEmpty(userGuideVOList)) {
                    return null;
                }
                userGuideVO = userGuideVOList.get(0);
            }
        } else if (StringUtils.isNoneBlank(guideCode)){
            userGuideVO = userGuideMapper.queryUserGuideByCode(guideCode);
        } else {
            return null;
        }
        if (userGuideVO == null) {
            return null;
        }

        List<UserGuideStepVO> userGuideStepVOList = userGuideStepMapper.listStepByGuideId(userGuideVO.getId());

        Set<Long> psIds = userGuideStepVOList.stream().map(UserGuideStepVO::getPermissionId).collect(Collectors.toSet());
        Set<Long> permittedIds = null;
        if (Boolean.TRUE.equals(userDetails.getAdmin())) {
            permittedIds = psIds;
        } else {
            if (projectId != null) {
                permittedIds = projectPermissionService.listProjectUserPermission(userDetails.getUserId(), psIds, projectId);
            } else {
                permittedIds = roleMemberService.listUserPermission(userDetails.getUserId(), psIds, organizationId);
            }
        }



        Set<Long> finalPermittedIds = permittedIds;
        userGuideStepVOList.forEach(userGuideStepVO -> {
            calculatePageUrl(projectId, organizationId, userGuideStepVO);
            userGuideStepVO.setPermitted(finalPermittedIds.contains(userGuideStepVO.getPermissionId()));
        });

        userGuideVO.setUserGuideStepVOList(userGuideStepVOList);
        return userGuideVO;
    }

    private void calculatePageUrl(Long projectId, Long organizationId, UserGuideStepVO userGuideStepVO) {
        String pageUrl = userGuideStepVO.getPageUrl();
        if (projectId != null) {
            ProjectDTO projectDTO = projectMapper.selectByPrimaryKey(projectId);
            pageUrl = pageUrl.replace("${projectId}", projectId.toString());
            if (projectDTO != null) {
                pageUrl = pageUrl.replace("${projectName}", projectDTO.getName());
            }
        }
        if (organizationId != null) {
            Tenant tenant = tenantMapper.selectByPrimaryKey(organizationId);
            pageUrl = pageUrl.replace("${organizationId}", organizationId.toString());
            if (tenant != null) {
                pageUrl = pageUrl.replace("${organizationName}", tenant.getTenantName());
            }
        }
        userGuideStepVO.setPageUrl(pageUrl);

    }
}
