package com.example.dataadmin.dto.task;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 手动提交试验任务评估的请求参数。 */
public class TaskEvaluationRequest {

    /** 调度系统下发的外部试验任务编号。 */
    @NotBlank(message = "试验任务编号不能为空")
    @Size(max = 100, message = "试验任务编号不能超过100个字符")
    private String taskId;

    /** 用户手动填写的试验任务评估内容。 */
    @NotBlank(message = "试验任务评估不能为空")
    private String taskEvaluation;

    /** 获取试验任务编号。 */
    public String getTaskId() {
        return taskId;
    }

    /** 设置试验任务编号。 */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /** 获取评估内容。 */
    public String getTaskEvaluation() {
        return taskEvaluation;
    }

    /** 设置评估内容。 */
    public void setTaskEvaluation(String taskEvaluation) {
        this.taskEvaluation = taskEvaluation;
    }
}
