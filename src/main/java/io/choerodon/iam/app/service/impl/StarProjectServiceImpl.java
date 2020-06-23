package io.choerodon.iam.app.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.iam.app.service.ProjectC7nService;
import io.choerodon.iam.app.service.StarProjectService;
import io.choerodon.iam.infra.constant.ResourceCheckConstants;
import io.choerodon.iam.infra.dto.ProjectDTO;
import io.choerodon.iam.infra.dto.StarProjectUserRelDTO;
import io.choerodon.iam.infra.mapper.StarProjectMapper;

/**
 * 〈功能简述〉
 * 〈〉
 *
 * @author wanghao
 * @since 2020/6/11 10:25
 */
@Service
public class StarProjectServiceImpl implements StarProjectService {

    private static final String ERROR_NOT_LOGIN = "error.not.login";
    private static final String ERROR_PROJECT_IS_NULL = "error.project.is.null";
    private static final String ERROR_SAVE_STAR_PROJECT_FAILED = "error.save.star.project.failed";
    private static final String ERROR_DELETE_STAR_PROJECT_FAILED = "error.delete.star.project.failed";

    @Autowired
    private StarProjectMapper starProjectMapper;
    @Autowired
    private ProjectC7nService projectC7nService;

    @Override
    @Transactional
    public void create(StarProjectUserRelDTO starProjectUserRelDTO) {
        // 数据校验
        projectC7nService.checkNotExistAndGet(starProjectUserRelDTO.getProjectId());
        Long userId = DetailsHelper.getUserDetails().getUserId();
        Assert.notNull(userId, ERROR_NOT_LOGIN);

        starProjectUserRelDTO.setUserId(userId);
        if (starProjectMapper.selectOne(starProjectUserRelDTO) == null) {
            if (starProjectMapper.insertSelective(starProjectUserRelDTO) > 1) {
                throw new CommonException(ERROR_SAVE_STAR_PROJECT_FAILED);
            }
        }

    }

    @Transactional
    @Override
    public void delete(Long projectId) {
        Assert.notNull(projectId, ERROR_PROJECT_IS_NULL);
        Long userId = DetailsHelper.getUserDetails().getUserId();
        Assert.notNull(userId, ERROR_NOT_LOGIN);

        StarProjectUserRelDTO record = new StarProjectUserRelDTO();
        record.setProjectId(projectId);
        record.setUserId(userId);
        StarProjectUserRelDTO starProjectUserRelDTO = starProjectMapper.selectOne(record);

        if (starProjectUserRelDTO != null) {
            if (starProjectMapper.deleteByPrimaryKey(starProjectUserRelDTO.getId()) > 1) {
                throw new CommonException(ERROR_DELETE_STAR_PROJECT_FAILED);
            }
        }

    }

    @Override
    public List<ProjectDTO> query(Long organizationId) {
        Assert.notNull(organizationId, ResourceCheckConstants.ERROR_ORGANIZATION_ID_IS_NULL);
        Long userId = DetailsHelper.getUserDetails().getUserId();
        Assert.notNull(userId, ResourceCheckConstants.ERROR_NOT_LOGIN);
        return starProjectMapper.query(organizationId, userId);

    }
}
