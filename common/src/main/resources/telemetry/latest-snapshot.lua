-- 每个参数独立过期。旧值不能覆盖较新值，也不能延长它的保留时间。
local retention = tonumber(ARGV[1])
local clock = redis.call('TIME')
local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
for index, key in ipairs(KEYS) do
    local incoming = ARGV[index + 1]
    local incomingTime = tonumber(string.match(incoming, '^([0-9]+)|'))
    local previous = redis.call('GET', key)
    local previousTime = previous and tonumber(string.match(previous, '^([0-9]+)|')) or -1
    local remaining = incomingTime + retention - now
    if remaining > 0 and incomingTime >= previousTime then
        redis.call('PSETEX', key, remaining, incoming)
    end
end
return 1
