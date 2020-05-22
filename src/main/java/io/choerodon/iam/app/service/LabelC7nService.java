package io.choerodon.iam.app.service;

import java.util.List;

import org.hzero.iam.domain.entity.Label;

/**
 * @author scp
 * @date 2020/3/31
 * @description
 */
public interface LabelC7nService {
    List<Label> listByOption(Label label);



    Label selectByName(String name);

    /**
     * 查询项目层gitlab标签
     * @return
     */
    List<Label> listProjectGitlabLabels();

}
