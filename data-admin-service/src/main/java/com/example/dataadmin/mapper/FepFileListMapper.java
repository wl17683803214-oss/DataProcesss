package com.example.dataadmin.mapper;

import com.example.dataadmin.vo.visualization.FepFileListItemVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 按任务和文件类别读取已上传的文件记录。 */
public interface FepFileListMapper {
    /** 统计当前任务和类别的文件数量。 */
    long count(@Param("taskId") String taskId, @Param("image") boolean image);
    /** 按接收时间倒序读取一页文件，并关联采集接口名称。 */
    List<FepFileListItemVO> findPage(@Param("taskId") String taskId,
            @Param("image") boolean image, @Param("offset") int offset,
            @Param("limit") int limit);
}
