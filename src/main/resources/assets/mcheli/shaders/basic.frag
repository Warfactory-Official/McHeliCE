#version 330 core

out vec4 FragColor;

in vec3 TextCoord;
in vec4 Color;
in vec3 Normal;

uniform sampler2D tex;
//uniform vec3 lightPos;
//uniform vec3 viewPos;
uniform vec3 color;

void main(){
    vec2 uv = aUVW.xy;
    FragColor = texture(tex, uv) * color;
}

