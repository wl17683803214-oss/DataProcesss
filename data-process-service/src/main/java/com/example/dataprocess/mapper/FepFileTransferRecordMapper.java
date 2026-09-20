package com.example.dataprocess.mapper;

import com.example.dataprocess.entity.FepFileTransferRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** FEP文件处理记录数据访问接口。 */
public interface FepFileTransferRecordMapper {

    /** 按协议能够确定的文件身份查询记录。 */
    FepFileTransferRecord findByIdentity(
            @Param("taskId") String taskId,
            @Param("interfaceId") Long interfaceId,
            @Param("fileName") String fileName,
            @Param("fileLength") long fileLength);

    /** 新增一条已经完成本地接收的文件记录。 */
    int insertReceived(FepFileTransferRecord record);

    /** 将需要重新接收的旧记录重置为已接收状态。 */
    int resetReceived(FepFileTransferRecord record);

    /** 查询到达重试时间且尚未发布的文件。 */
    List<FepFileTransferRecord> findRetryable(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    /** 将文件更新为已经上传。 */
    int markStored(
            @Param("id") Long id,
            @Param("fileUrl") String fileUrl);

    /** 将文件更新为已经发布。 */
    int markPublished(@Param("id") Long id);

    /** 保存一次后续处理失败信息和下次重试时间。 */
    int markRetryFailure(
            @Param("id") Long id,
            @Param("lastError") String lastError,
            @Param("nextRetryTime") LocalDateTime nextRetryTime);
}
