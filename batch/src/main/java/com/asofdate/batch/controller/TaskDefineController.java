package com.asofdate.batch.controller;

import com.asofdate.batch.entity.TaskArgumentEntity;
import com.asofdate.batch.entity.TaskDefineEntity;
import com.asofdate.batch.service.TaskDefineService;
import com.asofdate.hauth.authentication.JwtService;
import com.asofdate.utils.Hret;
import com.asofdate.utils.JoinCode;
import com.asofdate.utils.RetMsg;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Created by hzwy23 on 2017/6/1.
 */
@RestController
@RequestMapping(value = "/v1/dispatch/task/define")
@Api("批次调度-任务定义管理")
public class TaskDefineController {
    private final Logger logger = LoggerFactory.getLogger(TaskDefineController.class);
    @Autowired
    private TaskDefineService taskDefineService;

    /*
     * http GET /v1/dispatch/task/define
     * 查询指定域中的所有任务定义信息
     * 如果指定域为空,则返回请求用户所属域的任务定义信息
     * */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "查询指定域中的所有已经定义的任务信息", notes = "如果请求查询的参数为空，则返回用户所在域中定义的任务信息")
    @ApiImplicitParam(required = true, name = "domain_id", value = "域编码")
    public List<TaskDefineEntity> getAll(HttpServletRequest request) {
        String domainId = request.getParameter("domain_id");
        if (domainId == null || domainId.isEmpty()) {
            domainId = JwtService.getConnUser(request).getDomainID();
        }
        return taskDefineService.getAll(domainId);
    }

    /*
     * 新增任务组
     * */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "新增任务信息", notes = "向系统中添加新的任务，任务必须是存储过程，shell脚本，cmd脚本，可执行jar包，二进制文件中的一种")
    public String add(@Validated TaskDefineEntity taskDefineEntity, BindingResult bindingResult, HttpServletResponse response, HttpServletRequest request) {
        // 校验参数信息
        if (bindingResult.hasErrors()) {
            for (ObjectError m : bindingResult.getAllErrors()) {
                response.setStatus(421);
                return Hret.error(421, m.getDefaultMessage(), null);
            }
        }

        RetMsg retMsg = taskDefineService.add(parse(request));
        if (retMsg.checkCode()) {
            return Hret.success(retMsg);
        }
        response.setStatus(retMsg.getCode());
        return Hret.error(retMsg);
    }

    /*
     * 删除任务组
     * */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String delete(HttpServletResponse response, HttpServletRequest request) {
        String json = request.getParameter("JSON");
        List<TaskDefineEntity> args = new GsonBuilder().create().fromJson(json,
                new TypeToken<List<TaskDefineEntity>>() {
                }.getType());

        RetMsg msg = taskDefineService.delete(args);
        if (msg.checkCode()) {
            return Hret.success(msg);
        }
        response.setStatus(msg.getCode());
        return Hret.error(msg);
    }


    /*
     * 更新任务组
     * */
    @RequestMapping(method = RequestMethod.PUT)
    @ResponseBody
    public String update(@Validated TaskDefineEntity taskDefineEntity, BindingResult bindingResult, HttpServletResponse response, HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            for (ObjectError m : bindingResult.getAllErrors()) {
                response.setStatus(421);
                return Hret.error(421, m.getDefaultMessage(), null);
            }
        }

        RetMsg retMsg = taskDefineService.update(parse(request));
        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }
        return Hret.success(retMsg);
    }

    @RequestMapping(value = "/argument", method = RequestMethod.GET)
    @ResponseBody
    public List<TaskArgumentEntity> getTaskId(HttpServletRequest request) {
        String taskId = request.getParameter("task_id");
        logger.debug("task id is:" + taskId);
        return taskDefineService.getTaskArg(taskId);
    }

    @RequestMapping(value = "/argument/sort", method = RequestMethod.PUT)
    @ResponseBody
    public String updateSort(HttpServletResponse response, HttpServletRequest request) {
        String sortId = request.getParameter("sort_id");
        String uuid = request.getParameter("uuid");
        logger.info("sort is:{},uuid is:{}", sortId, uuid);
        RetMsg retMsg = taskDefineService.updateArgumentSort(sortId, uuid);

        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }
        return Hret.success(retMsg);
    }

    @RequestMapping(value = "/argument/delete", method = RequestMethod.POST)
    @ResponseBody
    public String deleteArg(HttpServletResponse response, HttpServletRequest request) {
        String uuid = request.getParameter("uuid");
        RetMsg retMsg = taskDefineService.deleteArg(uuid);
        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }
        return Hret.success(retMsg);
    }

    @RequestMapping(value = "/argument/type", method = RequestMethod.GET)
    @ResponseBody
    public TaskArgumentEntity getArgType(HttpServletResponse response, HttpServletRequest request) {
        String argId = request.getParameter("arg_id");
        return taskDefineService.getArgType(argId);
    }

    /*
     * 更新任务参数的值，在任务定义过程中，只能更新参数类型为任务类型的参数
     *
     * */
    @RequestMapping(value = "/argument/value", method = RequestMethod.POST)
    @ResponseBody
    public String updateArgValue(HttpServletResponse response, HttpServletRequest request) {
        String argValue = request.getParameter("arg_value");
        String uuid = request.getParameter("uuid");
        if (argValue == null || argValue.isEmpty()) {

        }
        RetMsg retMsg = taskDefineService.updateArgValue(argValue, uuid);
        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }
        return Hret.success(retMsg);
    }


    @RequestMapping(value = "/argument/add", method = RequestMethod.POST)
    @ResponseBody
    public String addArg(HttpServletResponse response, HttpServletRequest request) {
        TaskArgumentEntity taskArgumentEntity = new TaskArgumentEntity();
        String taskId = request.getParameter("task_id");
        if (taskId == null || taskId.isEmpty()) {
            response.setStatus(421);
            return Hret.error(421, "任务参数为空,请联系管理员", null);
        }
        taskArgumentEntity.setTaskId(taskId);

        String argId = request.getParameter("arg_id");
        if (argId == null || argId.isEmpty()) {
            response.setStatus(421);
            return Hret.error(421, "请选择参数", null);
        }
        taskArgumentEntity.setArgId(argId);
        String sortId = request.getParameter("sort_id");
        if (sortId == null || sortId.isEmpty()) {
            response.setStatus(421);
            return Hret.error(421, "参数排序号不能为空", null);
        }
        taskArgumentEntity.setSortId(sortId);
        taskArgumentEntity.setDomainId(request.getParameter("domain_id"));
        taskArgumentEntity.setArgValue(request.getParameter("arg_value"));

        RetMsg retMsg = taskDefineService.addArg(taskArgumentEntity);
        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }
        return Hret.success(retMsg);
    }

    private TaskDefineEntity parse(HttpServletRequest request) {
        String userId = JwtService.getConnUser(request).getUserId();
        TaskDefineEntity taskDefineEntity = new TaskDefineEntity();
        String taskId = JoinCode.join(request.getParameter("domainId"), request.getParameter("taskId"));
        taskDefineEntity.setTaskId(taskId);
        taskDefineEntity.setCodeNumber(request.getParameter("taskId"));
        taskDefineEntity.setCreateUser(userId);
        taskDefineEntity.setModifyUser(userId);
        taskDefineEntity.setDomainId(request.getParameter("domainId"));
        taskDefineEntity.setTaskDesc(request.getParameter("taskDesc"));
        taskDefineEntity.setTaskType(request.getParameter("taskType"));
        taskDefineEntity.setScriptFile(request.getParameter("scriptFile"));
        return taskDefineEntity;
    }
}