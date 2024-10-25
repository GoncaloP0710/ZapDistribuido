# Data-Privacy-and-Security-Project

## Authors

- André Reis - fc58192
- Daniel Nunes - fc58257
- Gonçalo Pinto - fc58178

## Project overview

### Network structure - Chord

<img src="READMEFiles/chord-p2p-removebg-preview.png" alt="plot" width="200"/>

De modo a criar um sistema pear-to-pear o maximo de descentralizado possivel e ao mesmo tempo eficiente, foi decidido implementar um sistema Chord permitindo que a busca de dados tenha complexidade $O(\log N)$, onde $N$ é o número de nós. Esta eficacia deve-se ao uso de `Finger Tables` em cada no de modo a ser possivel cortar caminho durante a procura de um no.

## Project Requirements

Para este projeto, o grupo decidiu utilizar a linguagem `Java`. Isto implica ter uma instalação do mesmo para conseguir executar o projeto.

Além disso, com base na nossa implementação do sistema Chord, o primeiro utilizador a entrar na rede é denominado como o `default node` e necessita de ter como IP, porta e nome: localhost, 8080 e "Wang", respetivamente. Este requirimento deve-se ao facto de facilitar o encontro com um node presente na network quando um novo se liga. Se este se desconectar a rede permanece funcional mas mais nenhum user se vai poder juntar.