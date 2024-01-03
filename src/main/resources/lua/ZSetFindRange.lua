local qname = KEYS[1]
local limit, lastMem = ARGV[1], ARGV[2]

local start = 0
local stop = limit - 1

if string.len(lastMem) > 0 then
    local lastIndex = redis.call('ZRANK', qname, lastMem)
    if lastIndex then
        start = lastIndex + 1
        stop = start + limit - 1
    end
end
return redis.call('ZRANGE', qname, start, stop)