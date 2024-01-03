local queueName = KEYS[1]
local lmt = ARGV[1]

local result = redis.call('ZRANGE', queueName, 0, lmt)
for i, ele in pairs(result) do
    if ele then
        redis.call('ZREM', queueName, ele)
    end
end
return result