local queueName = KEYS[1]
local msgId, priority = ARGV[1], ARGV[2]

redis.call('ZADD', queueName, priority, msgId)