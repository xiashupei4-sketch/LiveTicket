local stockKey = KEYS[1]
local userKey = KEYS[2]
local userId = ARGV[1]

local stock = tonumber(redis.call('GET', stockKey) or '-1')

if stock <= 0 then
    return 1
end

if redis.call('SISMEMBER', userKey, userId) == 1 then
    return 2
end

redis.call('DECR', stockKey)
redis.call('SADD', userKey, userId)

return 0
