# TheFirstPerson ResourcePack
___
> A set of core shaders and models for client-side first-person animation.

*This article is currently WIP.*

## How to use?
___
Let's spawn a right arm.<br/>
First, you need an item display entity. This one will hold the model.<br/>
`/summon minecraft:item_display ~ ~ ~ {transformation:{translation:[0.0f,-1024.0f,0.0f],left_rotation:[0.0f,0.0f,0.0f,1.0f],right_rotation:[0.0f,0.0f,0.0f,1.0f],scale:[1.0f,1.0f,1.0f]},item_display:"thirdperson_righthand",Tags:["right_arm"]}`<br/>
And then, we need a player head that will be the model.<br/>
`/give @s minecraft:player_head[minecraft:profile="<player name>",minecraft:custom_model_data=1]`<br/>
Also you should replace `<player name>` with the name of skin holder.<br/>
Next step is making the display hold the item. Select the item and run the command below<br/>
`/item replace entity @e[tag=right_arm,limit=1,sort=nearest] container.0 from entity @s weapon.mainhand`<br/>
Tada! It's almost done. Thanks to our core shader, it is rendered as a right arm.<br/>
Lastly we will make the item display ride the player.<br/>
`/mount <from> <to>`<br/>
This way, the player arm model will stick to the center of the screen.
## How does this work?
___
