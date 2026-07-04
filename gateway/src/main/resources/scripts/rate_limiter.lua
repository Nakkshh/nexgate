-- Sliding window rate limiter
-- KEYS[1] = rate limit key (e.g. "ratelimit:tenant_id")
-- ARGV[1] = current timestamp in ms
-- ARGV[2] = window size in ms (60000 = 1 min)
-- ARGV[3] = max requests allowed in window

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local windowStart = now - window

-- Remove entries older than the window
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- Count requests currently in the window
local count = redis.call('ZCARD', key)

if count < limit then
    -- Add this request with score = timestamp, unique member via timestamp+random
    redis.call('ZADD', key, now, now .. '-' .. math.random())
    redis.call('PEXPIRE', key, window)
    return {1, limit - count - 1}  -- allowed, remaining
else
    return {0, 0}  -- blocked, remaining = 0
end