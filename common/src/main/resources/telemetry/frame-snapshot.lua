-- 列表从新到旧保存完整记录，容量计数与裁剪在同一个脚本中完成。
local capacity = tonumber(ARGV[1])
local retention = tonumber(ARGV[2])
local readLimit = tonumber(ARGV[3])
local clock = redis.call('TIME')
local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
local micros = tonumber(clock[1]) * 1000000 + tonumber(clock[2])
local head = redis.call('LINDEX', KEYS[1], 0)
local lastOrder = head and tonumber(string.match(head, '^[0-9]+|([0-9]+)|')) or 0
local total = tonumber(redis.call('GET', KEYS[2]))
if redis.call('EXISTS', KEYS[1]) == 0 then
    total = 0
elseif not total then
    -- 计数键被Redis独立淘汰时，仅在恢复时重新计算已有记录字节数。
    total = 0
    for _, record in ipairs(redis.call('LRANGE', KEYS[1], 0, -1)) do
        total = total + string.len(record)
    end
end

-- 用记录的过期时间判定是否可见，读操作也会清理到期的尾部记录。
local function expiry(record)
    return tonumber(string.match(record, '^([0-9]+)|'))
end
local function removeTail()
    local record = redis.call('RPOP', KEYS[1])
    if record then
        total = total - string.len(record)
    end
end
local tail = redis.call('LINDEX', KEYS[1], -1)
while tail and expiry(tail) <= now do
    removeTail()
    tail = redis.call('LINDEX', KEYS[1], -1)
end

-- 一帧只写入所属接口和状态。预算不足时优先淘汰列表尾部旧帧。
local wrote = false
local assigned = {}
for index = 4, #ARGV do
    local record = ARGV[index]
    -- 用Redis的同槽执行顺序分配位置，同毫秒和多处理实例都能准确排序。
    local order = math.max(lastOrder + 1, micros)
    local separator = string.find(record, '|', 1, true)
    record = string.sub(record, 1, separator) .. string.format('%.0f', order) .. '|'
        .. string.sub(record, separator + 1)
    local size = string.len(record)
    if size <= capacity and expiry(record) > now then
        while total + size > capacity and redis.call('LLEN', KEYS[1]) > 0 do
            removeTail()
        end
        redis.call('LPUSH', KEYS[1], record)
        total = total + size
        wrote = true
        lastOrder = order
        assigned[#assigned + 1] = order
        assigned[#assigned + 1] = size
    else
        assigned[#assigned + 1] = 0
        assigned[#assigned + 1] = 0
    end
end
if total > 0 then
    -- 读操作保留键原有寿命，不为记录续期。
    local ttl = wrote and retention or redis.call('PTTL', KEYS[1])
    if ttl > 0 then
        if wrote then redis.call('PEXPIRE', KEYS[1], ttl) end
        redis.call('PSETEX', KEYS[2], ttl, total)
    end
else
    redis.call('DEL', KEYS[2])
end
if readLimit > 0 then
    local result = {}
    for _, record in ipairs(redis.call('LRANGE', KEYS[1], 0, readLimit - 1)) do
        if expiry(record) > now then
            result[#result + 1] = record
        end
    end
    return result
end
return assigned
