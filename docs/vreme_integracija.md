Weather penalties are applied dynamically during routing, using the latest `WeatherSnapshot`. The weather snapshot is loaded once at the start of the route calculation and then reused throughout that request.

When it is raining:
- `WALK` costs are multiplied by `2.0`.
- `BIKE` costs are multiplied by `5.0`.
- Bus transfer penalty is reduced by half.
- `WALK` edges longer than `500 m` are rejected.

When temperature is below `0°C`:
- `WALK` costs are multiplied by `1.5`.
- `BIKE` costs are multiplied by `2.0`.

When temperature is between `0°C` and `10°C`:
- `WALK` costs are multiplied by `1.2`.
- `BIKE` costs are multiplied by `1.3`.

When temperature is above `30°C`:
- `WALK` costs are multiplied by `1.3`.
- `BIKE` costs are multiplied by `1.1`.

`TRANSFER` legs are not treated as walking for weather penalties. In the latest implementation, `TRANSFER` means changing from one bus to a different bus at the same stop, with zero distance.

If no weather data is available, routing uses neutral weather and applies no weather-specific penalties.