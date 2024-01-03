local queueName = KEYS[1]

local result = redis.call('ZRANGE', queueName, 0, 0)
local ele = result[1]
if ele then
    redis.call('ZREM', queueName, ele)
    return ele
else
    return nil
end
