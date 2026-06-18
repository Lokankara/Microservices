Scenario: Resource upload flow

Given resource service is up
When I upload an mp3 file
Then I receive 200 with id
And processor receives message
